# BTPTesterAndroid

Bluetooth Tester application implementing BTP protocol. Uses WebSockets 
to communicate with the [BTPTesterCore](https://github.com/JuulLabs-OSS/BTPTesterCore) tool.
Allows testing of GAP and GATT profiles. 

#### Usage

- Enable USB debugging
- Keep the screen on
- Upload the app to an Android phone
- Enable Bluetooth, Location and Internet options
- Make sure that the phone is reachable from the BTPTesterCore tool
- Put the serial number of the phone into the BTPTesterCore configuration (`adb devices -l`)
- After that the app should be able to communicate with the BTPTesterCore and require no user intervention


More information here: [BTPTesterCore](https://github.com/JuulLabs-OSS/BTPTesterCore)
