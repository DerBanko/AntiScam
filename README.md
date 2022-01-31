# ⚠️ AntiScam Discord Bot

A simple Discord bot that helps you to manage sent [Scam](https://de.wikipedia.org/wiki/Scam) Messages on your Discord
Server.

## Usage

1. First, you will have to [invite](https://banko.tv/r/invite-antiscam) the Discord Bot to your Discord Server.
1. If you invite the Bot, use the Command `/antiscam log <channel>` to configure the Log Channel where a message should
   get sent every time someone sends a scam message to your Discord Server.
1. Then configure the punishment that should be executed when a scam message is sent
   with `/antiscam punishment <punishment>`

## Building

This project uses [Gradle](https://gradle.org) for its build process. You can build the project using:

```bash
./gradlew build
```

zip and tar distributions can be then found in `build/distributions`.

You can create a local unzipped distribution with:

```bash
./gradlew installDist
```

## Punishments

- Message Delete
- Kick Member
- Ban Member
- Timeout Member \<duration in seconds\>

## Found a not detected Link?

You can add the link to your server blacklist with `/antiscam add <URL>`. If the Bot owner approves the link, it will
get added to the global banlist.

- Contribute to the [discord-phishing-links](https://github.com/nikolaischunk/discord-phishing-links) repository to make
  the suspicious domain visible for everyone.

## Commands available

- `/antiscam log <channel>`
- `/antiscam punishment <punishment>`
- `/antiscam add <URL>`
- `/antiscam remove <URL>`

# Invite

https://banko.tv/r/invite-antiscam
