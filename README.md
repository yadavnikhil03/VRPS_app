# Vertical Rotary Parking System - Android App

This is a professional Android application for monitoring and controlling a Vertical Rotary Parking System, designed with Google's Material 3 principles.

## Features
- **Real-time Dashboard**: Monitor system status, occupancy, and available slots.
- **Dynamic Visualization**: Graphical representation of the parking structure.
- **Remote Control**: Start/Stop motors and manage maintenance modes.
- **History Logs**: Track parking and retrieval activities.

---

## 🚀 Steps to make this an app in Android Studio

Follow these steps to get the project running on your machine:

### 1. Open Android Studio
- Launch **Android Studio** (Hedgehog or newer recommended).
- On the Welcome screen, click **Open**.

### 2. Import the Project
- Navigate to the directory where you saved this project: `d:\Vertical_rotatory_parking_system`.
- Select the **folder itself** (it should show a Gradle icon) and click **OK**.
- Android Studio will now import the project and start a "Gradle Sync". Wait for this to finish (it might take a few minutes as it downloads dependencies).

### 3. Verify Build Settings
- Once the sync is complete, ensure the **app** module is selected in the run configuration (top right corner).
- Go to `File > Project Structure` and verify:
    - **SDK Version**: Compile & Target SDK should be **34**.
    - **JDK Version**: Ensure you are using **JDK 17** (standard for modern Android).

### 4. Run the App
- Connect a physical Android device or start an **Android Emulator**.
- Click the green **Run** (Play) button in the top toolbar.

---

## 🛠️ Project Structure
- `/app`: The main Android application module.
    - `src/main/java`: Java source code.
    - `src/main/res`: Layouts, drawables, and UI resources.
- `build.gradle`: Root-level build configuration.
- `settings.gradle`: Project structure definition.

## 🎨 UI & Aesthetics
The app uses **Material 3 (M3)** for a premium, modern feel:
- **Glassmorphism-inspired Cards**: Elevate the dashboard experience.
- **Dynamic Color Palettes**: Accessible and high-contrast UI.
- **Micro-animations**: Smooth transitions between Fragments.
