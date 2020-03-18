`BackgroundDetector` is an Android library written in Kotlin to provide tracking of foreground/background application status.

### Integration

At the project level `build.gradle`, add a maven repo pointing to `https://dl.bintray.com/vit-khudenko/libs`, e.g.:

```groovy
allprojects {
    repositories {
        google()
        jcenter()
        maven { url 'https://dl.bintray.com/vit-khudenko/libs' } // this is it
    }
}
```

At a module level `build.gradle`, add the following dependency:

```groovy
implementation 'vit.khudenko.android:background-detector:0.1.1'
```

### How does it work?

App is assigned with `background` status when none of app's activities are in `started` state.
And vice versa - if at least one activity in app is in `started` state, then app is assigned
with `foreground` status.

This approach has an undesired side effect: configuration changes, such as screen orientation changes,
would cause a short (typically 200-500 millis) period of `background` status followed by regaining
the `foreground` status. To mitigate this issue `BackgroundDetector` applies a status change debounce
delay which defaults to 700 ms (it is possible to supply a custom value via `debounceDelayMillis` param).

In order to track `started` state of activities, `BackgroundDetector` uses an implementation of
`Application.ActivityLifecycleCallbacks` reacting on `onActivityStarted(activity: Activity)` and 
`onActivityStopped(activity: Activity)` invocations.

By default `BackgroundDetector` takes under account all activities in an app, however if your app requires
some activities to be ignored, then a custom implementation of `BackgroundDetectorConfig` can do the trick
(check the `BackgroundDetectorConfig.shouldActivityBeProcessed(activity: Activity): Boolean`).

### Importance of a proper instantiation

For proper functioning `BackgroundDetector` must be instantiated at your app's
`Application.onCreate()` callback. `BackgroundDetector` does not validate
correctness of its instantiation, so if this requirement is not met, then there will be no
warnings despite `BackgroundDetector` may report incorrect status.

### Threading

The implementation is not thread-safe. `BackgroundDetector` must be accessed from the main thread only.

### License

> MIT License
> 
> Copyright (c) 2019 Vitaliy Khudenko
> 
> Permission is hereby granted, free of charge, to any person obtaining a copy
> of this software and associated documentation files (the "Software"), to deal
> in the Software without restriction, including without limitation the rights
> to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
> copies of the Software, and to permit persons to whom the Software is
> furnished to do so, subject to the following conditions:
> 
> The above copyright notice and this permission notice shall be included in all
> copies or substantial portions of the Software.
> 
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
> IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
> FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
> AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
> LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
> SOFTWARE.