# AndroidManifest.xml changes

Only two things change in `app/src/main/AndroidManifest.xml` – keep everything else from the starter.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".GeminiApp"            <!-- 1. add this -->
        ... keep the starter's other attributes ... >

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"   <!-- 2. add this -->
            ... >
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

No RECORD_AUDIO permission is needed: `RecognizerIntent` hands the microphone to the
system speech recogniser, which asks for its own permission.

(Remove the XML comments above when you paste – comments are not allowed inside a tag.)
