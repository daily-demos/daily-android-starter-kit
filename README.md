# A video chat app using Daily's Client SDK for Android

This demo showcases a basic video chat app that uses Daily's native [Android SDK](https://docs.daily.co/guides/products/mobile#introducing-dailys-native-mobile-libraries-beta) mobile library.

## Prerequisites

- [Sign up for a Daily account](https://dashboard.daily.co/signup).
- [Create a Daily room](https://help.daily.co/en/articles/4202139-creating-and-viewing-rooms), and then enter that URL into the demo. Manual room creation is only recommended for testing purposes -- in a production app we recommend creating Daily rooms [programmatically through our REST API](https://docs.daily.co/reference/rest-api/rooms/create-room).
- Install [Android Studio](https://developer.android.com/studio) and its prerequisites. The [install](https://developer.android.com/studio/install) and [run](https://developer.android.com/studio/run) instructions ought to cover what you need.

## How the demo works

In the demo app, a user must enter a URL for a [Daily room](https://docs.daily.co/reference#rooms), then press Join. The app will find the meeting room and join the call.

Most of the call-related logic in the app can be found in `DemoCallService`. This initializes a Daily `CallClient` instance, which is responsible for:

* Establishing a connection to the meeting room
* Keeping track of other participants, including their audio and video tracks
* Managing call settings, such as camera/microphone configuration
* Performing actions such as sending custom messages, or leaving the call

The call client is destroyed when the application exits.

When testing or running this demo, you can use a room you've manually created for calls. A production application should use the [Daily REST API](https://docs.daily.co/reference/rest-api) to create rooms on the fly for your users, which necessitates the use of a sensitive Daily API key. For security reasons, you likely don't want to embed this key in a production app. We recommend running a web server and keeping sensitive things like API keys there instead.

Please note this project is designed to work with rooms that have [privacy](https://www.daily.co/blog/intro-to-room-access-control/) set to `public`. If you are hardcoding a room URL, please bear in mind that token creation, pre-authorization, and knock-for-access have not been implemented here, meaning you may not be able to join non-public meeting rooms using this demo for now.

## Running locally

1. Clone this repository locally, i.e.: `git clone git@github.com:daily-demos/daily-android-starter-kit.git`
2. Open the directory in Android Studio.
3. Install dependencies by syncing your Android Studio project's gradle files using the Android Studio UI popups.
4. Build the project.
5. Run the project, either on a simulator (which will not have webcam access, but which can send an image in place of a real camera stream), or on a connected Android device.
6. Connect to the room URL you are testing.
7. To view a remote participant, connect again either in another simulator, on another device, or using a web browser. Careful of mic feedback! You might want to mute one or both sides' audio if they're near each other.

## Contributing and feedback

Let us know how experimenting with this demo goes! Feel free to [open an issue](https://github.com/daily-demos/daily-android-starter-kit/issues), or reach us any time at `help@daily.co`.
