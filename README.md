# Torque Plugin for Android Auto

This repository contains a customized version of the **AA Torque** performance monitor. 

**Acknowledgments:**
This project is a clone of the original [aa-torque](https://github.com/agronick/aa-torque) repository developed by [agronick](https://github.com/agronick). It builds upon their excellent work, which in turn was inspired by [Chillout's Performance Monitor](https://github.com/jilleb/mqb-pm) and Martoreto's aa-stats. 

## Overview

AA Torque is a highly customizable Android Auto performance monitor that interfaces with [Torque Pro](https://play.google.com/store/apps/details?id=org.prowl.torque&hl=en_US&gl=US). While the original predecessors focused heavily on VAG (Volkswagen Auto Group) vehicles, this version is designed to expose *all* available data from Torque in the most customizable way possible. 

You can retrieve and display any PID from Torque directly on your vehicle's Android Auto head unit.

## Key Features

- **Custom PIDs:** Display any data point accessible via Torque.
- **Extensive Customization:** Choose from various themes, fonts, icons, and backgrounds.
- **Multiple Layouts:** Create and swipe through up to 10 distinct dashboard screens.
- **Input Support:** Fully supports rotary dial controls on your vehicle's infotainment system.
- **Advanced Logic:** Utilize custom math and logic expressions powered by [EvalEx](https://ezylang.github.io/EvalEx/references/functions.html).
- **Configuration Management:** Easily backup, restore, and share your customized dashboard layouts.

## Installation Guide

Due to restrictions placed by Google on Android Auto applications, this app cannot be installed directly from the Play Store and requires a few extra steps.

### Prerequisites
1. **Torque Pro:** Must be installed on your Android device.
2. **OBD2 Adapter:** A compatible Bluetooth OBD2 reader connected to your car.

### Step-by-Step Installation
1. **Enable Developer Mode in Android Auto:**
   - Open the Android Auto app on your phone (disconnected from your car).
   - Scroll down to the "Version" or "About" section.
   - Tap the version number 10 times until you see a prompt confirming you are a developer.
   - Open the three-dot menu in the top right, select **Developer Settings**, and enable **Unknown Sources**.
2. **Download the App:**
   - Grab the latest APK release from the [original repository releases page](https://github.com/agronick/aa-torque/releases).
3. **Install the APK:**
   - Install the downloaded APK using your default package installer.
4. **Force Update via Settings:**
   - Open the **AA Torque Settings** app on your phone.
   - Go to the options menu and select "Force Update." Follow the prompts to reinstall the app so your device registers it as a Play Store installation.
   
*(Note: If the standard installation fails, you may need to use [KingInstaller](https://github.com/fcaronte/KingInstaller/releases). Please refer to the KingInstaller documentation, especially if you are using a Google Pixel device or running Android 14+).*

## Usage

1. **Grant Permissions:** Open the AA Torque Settings app on your phone and grant all requested permissions. **The app will not function without them.**
2. **Connect to Car:** Plug your phone into your car's USB port (or connect wirelessly) and launch Android Auto.
3. **Launch the App:** Open the Android Auto app drawer on your car's screen and look for the AA Torque icon (it resembles a dashboard clock).

## Custom Expressions

This application uses EvalEx to evaluate custom mathematical expressions for your gauges and readouts. For a full list of supported functions and operators, please refer to the [EvalEx Documentation](https://ezylang.github.io/EvalEx/references/functions.html).

## Support and Community

- **Original Discussions:** Visit the [original repo's discussion page](https://github.com/agronick/aa-torque/discussions) for help and community support.
- **Translations:** Help translate the app into your native language via [POEditor](https://poeditor.com/join/project/yttme0y1VZ).
- **Donations:** If you love this project, consider supporting the original developer via [PayPal](https://www.paypal.me/kagronick) or [GitHub Sponsors](https://github.com/agronick).
