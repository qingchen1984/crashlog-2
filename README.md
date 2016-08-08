# crashlog

##Features
* catch the crash,save to log file
* check current start is or not from crash
* it works well at Android-23 or later
* auto open the log file when crash accured

grab via Gradle:

compile 'com.simen.crashlog:crashlog:1.2.0'

##sample code
```Android
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(this);
    }
}
```

License
--------

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.