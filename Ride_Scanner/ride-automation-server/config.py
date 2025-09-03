# ride-automation-server/config.py
APPIUM_SERVER_URL = "http://localhost:4723/wd/hub"
DEVICE_CONFIG = {
    "platformName": "Android",
    "deviceName": "emulator-5554",
    "automationName": "UiAutomator2",
    "newCommandTimeout": 300,
    "noReset": True
}

APP_PACKAGES = {
    "uber": "com.ubercab",
    "ola": "com.olacabs.customer", 
    "rapido": "com.rapido.passenger"
}
