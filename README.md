# Pacemaker

GCM (or Google Cloud Messaging) is an amazing tool that you could use in a variety of ways within your Android app. It had [its flaws](https://blog.pushbullet.com/2014/02/12/keeping-google-cloud-messaging-for-android-working-reliably-techincal-post/), but when used correctly, it allowed developers to get a lot done with very little work, that too for free with no rate limits (albeit potential throttling if you went nuts).

Google chose to create GCM largely as a black box, where you were simply told if a push message has been successfully received by the server, and then passing that push message on to your app. You would have fairly little information regarding what happened in between, and for most developers that was fine. Debugging on a few devices would rarely show any errors, with pushes delivered instantaneously.

However, if you send a large enough volume of push messages, you might have noticed a few times that the numbers simply don't add up. Reading and re-reading your code would have told you that everything's fine, and that maybe users simply haven't had access to internet or have uninstalled the app itself. Turns out, that's not always the case. There's a [pretty serious bug out there](http://forum.xda-developers.com/showthread.php?t=2142503) that could be dropping your delivery success rate significantly.

While the link above goes deeply into it, I'll summarize here: Play Services sends a heartbeat to its servers every 15 minutes on WiFi and 28 minutes on mobile networks to ensure constant connectivity, even when the user doesn't touch his device. This is done to prevent a TCP idle timeout from breaking the connection. Under most circumstances, this is ok. Unfortunately, a number of WiFi routers, OEMs, ISPs and mobile carriers actually perform a timeout much faster, which results in long periods when a push simply cannot be delivered to a user.

This issue was spotted earlier by a few developers who went on to develop apps that "fixed" push notifications for you. However, as a developer with Haptik, I couldn't rely on our users having this app installed. So, to solve this issue, I ended up adding the functionality into our app itself. The impact has been exactly what we needed: looking through the Developer Console data, we can see for ourselves that issues with GCM messages stuck in "accepted" state has dropped to a negligible amount (in fact, I haven't seen any stuck) with all of them making it through to our users devices.

Right from the day we built out Pacemaker into our app, our goal was to offer the benefits of the library to other developers. So go ahead, check through your GCM data to see if you are having delivery issues, and add this to your project if you want to fix it.

## How to use

First, add the library to your project using Maven Central

    repositories {
        maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
    }

    dependencies {
        compile 'in.raveesh:pacemaker:0.1.0-SNAPSHOT'
    }

We've exposed two functions to start Pacemaker, one which sends linear heartbeats, and one which sends them after exponential gaps.

###### Linear

    /**
     * Starts a linear repeated alarm that sends a broadcast to Play Services, which in turn sends a heartbeat
     * @param context Context from your application
     * @param delay Gap between heartbeats in minutes
     */
    Pacemaker.scheduleLinear(Context context, int delay)

###### Exponential

    /**
     * Starts an exponential alarm that sends a broadcast to Play Services, which in turn sends a heartbeat
     * @param context Context from your application
     * @param delay Time in which to send first broadcast. Subsequent broadcasts would be at exponential intervals
     * @param max The max time till which the broadcasts should be sent. Once past this limit, no more heartbeats are sent
     */
    Pacemaker.scheduleExponential(Context context, int delay, int max) 

#### Which should I use
For apps, such as Haptik, where it can be reasonably guessed when a push notification is most likely to be received, the exponential system works well since it does not result in heartbeats being sent continuously. For example, at Haptik, it is far more likely to receive a push immediately after a user moves out of the app, and rarely after a significant portion of time.

For apps, such as social messaging applications, where a message might come at any time and it is important to notify the user immediately, the linear option would be the right one. Please be careful with regards to the time gap that you choose.

## Impact on battery
In our testing (including real world data), we have seen no noticeable impact of the exponential option on battery life. We haven't used the linear system, but we believe if you need that, your only real alternative is to keep a service running constantly in the background, which would be worse.

## TODO
- ~~Add library to Maven Central~~
- Develop a method by which multiple apps using Pacemaker on the same device work together so as to not constantly send heartbeats independently, thereby reducing any impact on battery life.

## Apps using Pacemaker
- [Haptik Personal Assistant](https://play.google.com/store/apps/details?id=co.haptik)

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Pacemaker-green.svg?style=flat)](https://android-arsenal.com/details/1/2216)
