

# Lolly
A new way to observe android app log in real time.

![](http://upload-images.jianshu.io/upload_images/2702499-689a9034c58afe80.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

# How to include  

Add the library to your module **build.gradle**:

```gradle
dependencies {
    implementation 'com.zql.android:lolly:2.0'
}
```

# Usage

AndroidManifest.xml

```xml
        <service
            android:name="com.zqlite.android.lolly.Lolly"
            android:enabled="true"
            android:exported="false"
            android:process=":remote" />
```

permissions

```xml
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.READ_LOGS" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
> you should request above runtime permissions by yourself

java api

```java
public static void Lolly.showLolly(android.app.Activity activity,
                             java.lang.String[] tags)

public static void Lolly.hideLolly(@NotNull android.app.Activity activity)

public static void Lolly.saveLog(@NotNull android.app.Activity activity)
```


**😆 Have fun with this function!**

# License

     Copyright 2016 zhangqinglian

  	Licensed under the Apache License, Version 2.0 (the "License");
  	you may not use this file except in compliance with the License.
  	You may obtain a copy of the License at

	     http://www.apache.org/licenses/LICENSE-2.0

  	Unless required by applicable law or agreed to in writing, software
	  distributed under the License is distributed on an "AS IS" BASIS,
	  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	  See the License for the specific language governing permissions and
	  limitations under the License.


