# AGENTS.md – ChamCong AI Coding Guide

## Project Overview
Android kiosk app for facial-recognition-based employee time attendance.
**Stack:** Kotlin · Jetpack Compose · CameraX · ML Kit · TFLite (MobileFaceNet) · Room · Supabase · WorkManager.

---

## Critical Pre-Build Requirement
**The app will not compile/run without a TFLite model file.**
Place `mobilefacenet.tflite` in `app/src/main/assets/`. Then verify constants in `ml/FaceEmbeddingHelper.kt`:
- `INPUT_SIZE = 160` (pixels – resize target before inference)
- `EMBEDDING_DIM = 512` (must match your model variant: 128 / 192 / 512)

Supabase credentials go in `local.properties` (gitignored):
```
supabase.url=https://...supabase.co
supabase.anon.key=eyJ...
```
They are injected into `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_ANON_KEY` via `app/build.gradle.kts`.

---

## Architecture
```
camera/FaceAnalyzer  →  ui/AttendanceViewModel  →  domain/ProcessAttendanceUseCase
                                 ↓                          ↓
                         data/repository/            ml/{FaceEmbeddingHelper,
                         AttendanceRepository         FaceMatcher, VectorMath}
                              ↓           ↓
                    data/local/       data/remote/
                    (Room DB)         SupabaseSyncManager
```
- **Single Activity** (`MainActivity.kt`) with Compose Navigation. No Fragments.
- **ViewModel Factory** pattern (no DI framework): see `AttendanceViewModel.Factory`.
- **Offline-first**: attendance is written to Room first (`isSynced = false`), then pushed to Supabase by `SupabaseSyncManager.syncAttendance()`.

---

## Face Recognition Pipeline
1. `FaceAnalyzer` (CameraX background thread): ML Kit detects face → crops square bitmap with 20% padding.
2. `ProcessAttendanceUseCase` (`Dispatchers.Default`): calls `FaceEmbeddingHelper.getEmbedding()` → L2-normalize → `FaceMatcher.findBestMatch()`.
3. Match threshold: `FaceMatcher.SIMILARITY_THRESHOLD = 0.90f` (cosine similarity). Tune here for your lighting conditions.
4. IN/OUT toggling: the last `AttendanceEntity.status` for today determines the next type (`IN → OUT → IN …`).

---

## Threading Rules
| Layer | Dispatcher |
|---|---|
| TFLite inference & vector math | `Dispatchers.Default` |
| Room / Supabase I/O | `Dispatchers.IO` |
| UI state emission | `Main` (via `StateFlow`) |
| Camera frame analysis | CameraX executor (background) |

Never run TFLite on `Dispatchers.IO` — it's CPU-bound. Never block the CameraX thread.

---

## Room Database
- **DB name:** `chamcong.db` · **Version:** 4 · Schema exported to `app/schemas/`.
- Face vectors stored as nested JSON string (`List<FloatArray>` ↔ `VectorTypeConverter`).
- **Never use `fallbackToDestructiveMigration`** — always add an explicit `Migration` in `TimeKeepingDatabase`.
- When adding a column: `ALTER TABLE … ADD COLUMN …` with a `DEFAULT` value.
- When changing a column type: create `_new` table, copy data, drop old, rename (see `MIGRATION_3_4`).

---

## Supabase Sync
- Tables: `employees` (columns: `id`, `name`, `face_vector TEXT`, `is_active BOOL`) and `attendance_logs` (`employee_id`, `scan_time` ISO8601 UTC, `status`, `device_id`).
- Remote models use `@SerialName` (`face_vector`, `is_active`, `employee_id`, `scan_time`) — see `data/remote/SupabaseModels.kt`.
- Upsert strategy in `fetchEmployees()`: if Supabase sends an empty `face_vector`, the local vector is preserved.
- `deviceId` hardcoded to `"KIOSK-01"` in `RemoteAttendanceLog`.

---

## Key Patterns
- **`AttendanceUiState`** is a sealed class (`Scanning`, `Processing`, `Matched`, `Unknown`, `Error`, `Paused`). Never add raw strings for new states — add a new `data object` or `data class` subtype.
- **TFLite model loading** is done manually via `AssetFileDescriptor` + `MappedByteBuffer` to avoid `tensorflow-lite-support` (conflicts with ML Kit's `litert-api`). Do not add `tensorflow-lite-support` as a dependency.
- **TFLite version is pinned to `2.13.0`** — newer versions conflict with ML Kit. See comment in `app/build.gradle.kts`.
- Face images are saved to `context.filesDir` as `face_<timestamp>.jpg` (not external storage).

---

## Build & Run
```powershell
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Room schema validation (run after any DB entity change)
./gradlew kspDebugKotlin
```
KSP processes Room annotations; always run `kspDebugKotlin` after changing any `@Entity` or `@Dao`.

---

## Key Files Reference
| File | Purpose |
|---|---|
| `ml/FaceEmbeddingHelper.kt` | TFLite inference; change `INPUT_SIZE`/`EMBEDDING_DIM` here |
| `ml/FaceMatcher.kt` | `SIMILARITY_THRESHOLD` tuning |
| `data/local/TimeKeepingDatabase.kt` | DB version & all migrations |
| `data/local/VectorTypeConverter.kt` | `List<FloatArray>` ↔ JSON serialization |
| `data/remote/SupabaseSyncManager.kt` | All Supabase read/write logic |
| `domain/ProcessAttendanceUseCase.kt` | Core attendance pipeline (embed → match → record) |
| `ui/AttendanceViewModel.kt` | UI state orchestration & `Factory` |
| `camera/FaceAnalyzer.kt` | CameraX frame → cropped face bitmap |

