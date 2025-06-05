⚠️⚠️⚠️ 注意：本项目（包括本文）使用了海量 AI 内容，欢迎您进行 fork 或修改继续改进，谢谢！

⚠️⚠️⚠️ Note: This project (including this README) makes extensive use of AI-generated content. You're welcome to fork it or build upon it. Thanks for your interest!

# Misogintb - NFC 刷卡计时系统  
**NFC Timing System with Session Management**

---

## 🧾 项目简介 | Project Overview

Misogintb 是一个适用于 Android 设备（最低支持 Android 5.0）的 NFC 刷卡计时系统。  
支持多用户刷卡上下机，自动记录时长与费用，全屏显示实时信息。

Misogintb is an Android-based NFC timing system for devices running Android 5.0 and above.  
It supports multi-user check-in/out via NFC, tracks session time & cost, and provides full-screen real-time display.

---

## 🚀 主要功能 | Key Features

- 实时大字显示当前日期与时间（黑底白字）
- 支持多种 NFC 标签：MifareClassic、FeliCa、IsoDep、NfcA、NfcB、NfcF、NfcV 等
- 刷卡开始计时，再次刷卡结束计时
- 自动计算使用时长与费用
- 上下机均需确认（支持倒计时自动操作）
- 支持最多 255 张不同 NFC 卡同时记录
- 屏幕常亮、横屏显示、自动旋转支持

---

## 🛠️ 构建方法 | How to Build

### 环境要求 | Requirements

- JDK: 11  
- Android Studio: 推荐 Arctic Fox (2020.3.1) 或更高版本  
- Android SDK:  
  - compileSdk = 28  
  - targetSdk = 28  
  - minSdk = 21  
- Gradle: 7.5.1  
- AGP (Android Gradle Plugin): 7.0.4

### 编译步骤 | Build Steps

```bash
git clone https://github.com/yourusername/misogintb.git
cd misogintb
# 使用 Android Studio 打开该项目
# 点击 “Run” 或菜单栏中的 “Build > Make Project”
````

---

## 📂 项目结构 | Project Structure

```
misogintb/
├── app/
│   └── src/
│       └── main/
│           ├── java/                 Kotlin 源代码
│           ├── res/                  UI 资源与布局
│           └── AndroidManifest.xml   应用配置
├── build.gradle
├── settings.gradle
├── gradle/
│   └── wrapper/
├── gradlew / gradlew.bat
├── .gitignore
└── README.md
```

---

## 🙋‍♀️ 联系作者 | Contact

如有建议、问题或合作意向，请通过 GitHub Issues 联系。
For suggestions, issues, or collaboration, feel free to open a GitHub Issue.

---

## ❤️ 致谢 | Acknowledgments

感谢所有开源社区、Android NFC 开发者提供的资料与示例。
Thanks to the open-source community and Android NFC developers for documentation and sample code.

```

---

如果你需要，我也可以帮你生成：

- `.gitignore`（推荐模板）
- `LICENSE`（MIT、Apache-2.0 等任选）
- `nfc_tech_filter.xml` 示例
- 最小发布包 `.zip`

你只需要告诉我即可。
```
