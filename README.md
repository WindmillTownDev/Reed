# Reed

Reed is the dedicated server core for WindmillTown Network. It is a fork from Paper and aimed at providing features that can only be implemented by modifying the server core. Reed works together with WindmillTownCore, the dedicated plugin for WindmillTown. To be more procise, Reed only provides APIs and WindmillTownCore makes use of these APIs to provide user interfaces to players.

Currently Reed is dedicated on patching Paper in these perspectives:

- Reverse Spigot and Paper patches that affect vanilla creative redstone behaviours, such as entity collisions, entity ticking, redstone optimizations and etc..
  
- Provide helpful redstone utilities that can only be implemented by modifying server, such as debug block and tickrate
  
- Improve redstone performance with the premise of not breaking vanilla redstone behaviour (todo)
  

## Build

Run ``gradle applyPatches`` to apply Reed patches to Paper source.

Then ``gradle createReobfBundlerJar`` to build Reed server jar file.

To develop WindmillTownCore, run ``gradle publishToMavenLocal`` so that Reed API will be built into your local maven repo.

## Personal Usage

You are allowed to use Reed server and its source in any non-commercial usage. But WindmillTown Develop Team will not guarantee any in-time supports.
