# Friend Creeper
> Tame Creepers with gunpowder and turn them into your loyal companion!
---
## Features
-  **Tame** — Right-click a Creeper with gunpowder to tame it. Each attempt has a 1/3 chance of success; it will always succeed on the 5th attempt. A tamed Creeper wears a poppy on its head.
-  **Follow** — Your tamed Creeper will follow you around and stay by your side.
-  **Protect** — When you are attacked, your Creeper will target the attacker and explode to defend you.
-  **Explode on enemies** — Your Creeper will detonate on hostile mobs that threaten you.
-  **Sit** — Right-click to make your Creeper sit. Right-click again to stand up. A sitting Creeper will not move or attack.
-  **Feed** — Right-click your injured Creeper with gunpowder to heal it for 4 HP. A heart particle will appear on success.
-  **Owner protection** — Your Creeper will never hurt you, even with arrows or explosions.
---
## How to Tame
1. Hold **gunpowder** in your hand.
2. Right-click a Creeper.
3. On success: green particles appear and a **poppy** blooms on its head. 
---
## Controls
| Action | How |
|---|---|
| Tame | Right-click with gunpowder |
| Sit / Stand | Right-click |
| Feed (heal) | Right-click with gunpowder when hurt |
---
## Behavior Notes
- Tamed Creepers **will attack** hostile mobs defending their owner, and will fight back against mobs that attack them.
- A sitting Creeper **ignores all threats** and stays put until told to stand.
- Untamed Creepers **will not explode** while you hold gunpowder nearby.
---
## Requirements
- Fabric Loader `>=0.19.2`
- [Fabric API](https://modrinth.com/mod/fabric-api) `>=0.116.11`
- Minecraft `1.21.1`
---
## Building from Source
```bash
git clone https://github.com/smalldayDC/friendlycreeper.git
cd friendlycreeper
gradlew build
```
The compiled jar will be in `build/libs/`.
