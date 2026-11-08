# GALADIMA JAKADA ACADEMY — Report Card V3.1

## Project contents
- `index.html`, `style.css`, `script.js`: offline report-card web app.
- `assets/`: school logo, proprietor signature, and official school stamp.
- `app/`: Android WebView application with Android PrintManager and ZXing QR generation/scanning.
- `.github/workflows/build-apk.yml`: GitHub Actions workflow for building a debug APK.

## Main features
- Nursery classes (6 default subjects) and Primary 1–5 (10 default subjects).
- CA /40, Exam /60, total /100. Grade bands: A 75–100, B 65–74, C 50–64, D 45–49, E 40–44, F 0–39.
- Subject lists can be managed per class and are saved locally on the device. Student marks are not saved as a student database.
- QR report data includes student identity, class/session/term, subject marks, totals, grades and comments. The Android app generates QR images locally using ZXing; scanning uses the camera or a Gallery image.
- Android PrintManager uses a print-job title based on student name, admission number, class, term and session.
- A4 portrait print stylesheet includes the school stamp and generation timestamp.

## Build APK with GitHub Actions
1. Upload the full contents of this project to the root of your GitHub repository (keep `.github/workflows/build-apk.yml`).
2. Open the repository's **Actions** tab.
3. Select **Build GJA Report Card APK** and choose **Run workflow** (or push to `main` / `master`).
4. When the workflow succeeds, open the run and download the `GJA-Report-Card-APK` artifact.
5. Extract the artifact and install `app-debug.apk` on the Android phone.

## Important verification note
The source includes native Android print and QR bridges, but a successful GitHub Actions build and on-device test are still required to verify the APK on a particular phone. The Android print dialog uses the print job name as its suggested document name; the final saved filename can still be edited by the person saving the PDF.
