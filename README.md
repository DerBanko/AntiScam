# ⚠️ AntiScam Discord Bot

A simple Discord bot that helps you to manage sent [Scam](https://en.wikipedia.org/wiki/Scam) Messages on your Discord
Server.

## Usage

1. First, you will have to [invite](https://banko.tv/r/invite-antiscam) the Discord Bot to your Discord Server.
2. If you invite the Bot, use the Command `/antiscam log <channel>` to configure the Log Channel where a message should
   get sent every time someone sends a scam message to your Discord Server.
3. Then configure the punishment that should be executed when a scam message is sent
   with `/antiscam punishment <punishment>`
4. (optional) Enable the violation system by using `/antiscam violation enable`. This new system checks messages for specific phrases like `gift`, `nitro`, ...

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

### Categories
(The categories can be optionally added in `/antiscam punishment <Punishment> [Category]`)

- Scam url found
- Medium violation (violation score 16-35)
- High violation (violation score 36-55)
- Extreme violation (violation score 56+)

## Commands available

- `/antiscam log <channel>`
- `/antiscam punishment <punishment> [category]`
- `/antiscam add <URL>`
- `/antiscam remove <URL>`
- `/antiscam list`
- `/antiscam violation enable`
- `/antiscam violation disable`

# Contribute

## Found a not detected URL?

You can add the link to your server blacklist with `/antiscam add <URL>`. If the Bot owner approves the link, it will
get added to the global banlist. Additionally you can contribute to the [discord-phishing-links](https://github.com/nikolaischunk/discord-phishing-links) repository.

## Help us translate AntiScam

Follow the guide in the [CONTRIBUTING.md](https://github.com/DerBanko/AntiScam/blob/master/CONTRIBUTING.md) file.

# Invite

https://banko.tv/r/invite-antiscam

# Special Thanks to our Translators

<contributions>
