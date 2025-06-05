⚠️⚠️⚠️注意，本项目（包括本文）使用了海量AI内容，欢迎您进行fork之类的操作继续改进，谢谢

⚠️⚠️⚠️ Note: This project (including this README) makes extensive use of AI-generated content. You're welcome to fork it or build upon it. Thanks for your interest!

# Misogintb - NFC刷卡计时系统  
NFC Timing System with Session Management

---

## 🧾 项目简介 | Project Overview

**Misogintb** 是一个为 Android 设备（最低支持 Android 5.0）开发的 NFC 刷卡计时系统。它支持多用户刷卡上/下机，自动记录使用时长和费用，并通过大字全屏显示实时信息。
**Misogintb** is an Android-based NFC timing system designed for devices running Android 5.0 and above. It supports multiple users tapping in/out with NFC cards, tracks session time and cost, and provides full-screen real-time display.

---

## 🚀 主要功能 | Key Features

- 实时大字显示当前日期和时间（黑底白字）
- 支持 MifareClassic、FeliCa、IsoDep、NfcA、NfcB 等常见 NFC 标签
- 刷卡开始计时，再次刷卡结束计时
- 自动计算使用时长与费用
- 上下机均需确认（支持倒计时自动处理）
- 支持最多 255 张不同 NFC 卡同时记录
- 屏幕常亮、横屏显示、自动旋转支持

---

## 🛠️ 构建方法 | How to Build

### 环境要求 | Requirements

- **JDK**: 11  
- **Android Studio**: 推荐 Arctic Fox (2020.3.1) 或更高  
- **Android SDK**: compileSdk = 28, targetSdk = 28, minSdk = 21  
- **Gradle**: 7.5.1  
- **AGP**: 7.0.4

### 编译步骤 | Build Steps

git clone https://github.com/yourusername/misogintb.git
cd misogintb
# 使用 Android Studio 打开项目
# 然后点击 "Run" 或 "Build > Make Project"

---

## 📂 项目结构 | Project Structure

misogintb/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/...         # Kotlin 代码
│   │   │   ├── res/             # UI 布局与资源
│   │   │   └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
├── gradle/
│   └── wrapper/
├── .gitignore
└── README.md

---

## 🙋‍♀️ 联系作者 | Contact

如有建议、问题或合作意向，请通过 GitHub Issue 联系。
For suggestions, issues, or collaboration, feel free to open an Issue on GitHub.

---

## ❤️ 致谢 | Acknowledgments

感谢所有开源社区、Android-NFC 开发者的资料与示例。
Thanks to the open-source community and Android NFC developers for documentation and samples.
