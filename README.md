# Asahi [![Modrinth Downloads](https://img.shields.io/modrinth/dt/asahi?color=00AF5C&label=modrinth&style=flat&logo=modrinth)](https://modrinth.com/mod/asahi)

Client side Minecraft mod that removes daylight cycle rubber-banding caused by server lag!

<img src="https://i.imgur.com/77kxz8x.png" alt="If you ask for forge I'll steal your kneecaps" width="200"/>
<br><br>
<p><b>The problem</b></p>
Every second (20 ticks,) the server sends a packet that syncs the world time between the client and the server. Since clients have no reference for the tick rate of the server, they assume that the server is running at the standard 20 ticks per second. As such, when the server is lagging, that 20 ticks takes longer to process, leading the client to display the sun ahead of where it is on the server. When that sync packet comes in the vanilla client snaps the sun to the server position, causing the issue that this mod fixes. 
<br><br>
<p><b>The solution</b></p>
A rolling weighted average, and some interpolation! First, we take samples of the last 10 seconds of the amount of ticks that passed on the server per tick on the client. Then, since the sample from 10 seconds ago isn't as relevant as the sample in the last second, we weigh each of them exponentially less compared to the first. Taking the average of this weighted list, we can effectively predict the movement of the sun over the next second. The margin of error in the suns position caused by the prediction varies, but it is imperceptbile in comparison to the snap-back that happens with the vanilla logic.
<br><br>
<p><b>Without Asahi:</b></p>
<img src="https://cdn.modrinth.com/data/CPo6Ht5f/images/75950312eda4a58e7ba63eba66aef024dc3be940.gif" alt="Daylight cycle without Asahi" width=600/>
<br><br>
<p><b>With Asahi:</b></p>
<img src="https://cdn.modrinth.com/data/CPo6Ht5f/images/9b1156f30b3dceda6d66a1087afe99ee5cb46e6d.gif" alt="Daylight cycle with Asahi" width=600/>

Please note that this gif is from an earlier version of the mod, the interpolation used has only gotten better since!

Circular Sun is part of a resource pack provided by [vanillatweaks.net](https://vanillatweaks.net/)

## Config

```json5
{
  // If the time received by the server is `skipDuration` or more seconds from the client time,
  // skip to the server's time, bypassing interpolation. 
  // Play around with this value if the daylight cycle is absurdly fast.
  "skipDuration": 60,
  // Amount of prior factors to use in the rolling average interpolation. 
  // Play around with this value if you notice the sun bouncing back and forth a lot without settling into position.
  "interpolateSamples": 10,
  // Initial daylight cycle tick per game tick. 
  // Increase/decrease this value if another mod is present that speeds up/slows down the daylight cycle  
  "initialFactor": 1.0,
  // The standard tick rate with relation to the resolution of the daylight cycle. 
  // Change if playing with mods that increase/decrease the standard tick rate OR the resolution of a daylight cycle (24000 ticks).
  // Note that to my knowledge there are no mods that do this.
  "standardTickRate": 20
}
```