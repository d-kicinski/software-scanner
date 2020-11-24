<p align="center"> <font size="20"> 🆂🅾🅵🆃🆆🅰🆁🅴 🆂🅲🅰🅽🅽🅴🆁 </font> </p>

<p align="center">
<img src="https://github.com/dawidkski/software-scanner/blob/master/res/cropped_optim.gif" width="350" height="600" />
</p>

Toy-project to learn how to use native code in Android applications. This sample provides scanner-like functionality. Would you like to take a photo of a document, paper, or recipe? Point your camera at it, click the shutter button, and wait for the result. Green contour will tell you how the implemented algorithm sees your document in real-time.

<p align="center">
<img src="https://github.com/dawidkski/software-scanner/blob/master/res/img2.png" width="640" height="360"/>
</p>

In addition, there's no need to worry about holding your smartphone straight as application will performs perspective transformation to fix that for you. :^)

#### Install boundled apk
To install apk boundled in the latest release open this [link](https://github.com/d-kicinski/software-scanner/releases/download/v0.2.0/scanner-app-arm64-v8a-release.apk) in your browser on your smartphone. Download the apk and install it. This apk is signed locally so probably you'll be warned about possible danger.

#### Build apk by yourself
To build and install apk on your device simply run the following.
```bash
./gradlew build installDebug
```

#### Using Android Studio
When you hit `Build` in Android Stdio it ignores my task for downloading external dependencies.
To build it you'll need to run the command `./gradlew build` first to fetch `scaner-jni` and `opencv` libs.
Then you can use Android Studio as you please :^)

If you're interested in how detection and transformation is performed inspect this [repository](https://github.com/dawidkski/software-scanner-native)
