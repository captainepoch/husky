# Husky

[![builds.sr.ht status](https://builds.sr.ht/~captainepoch/husky/commits.svg)](https://builds.sr.ht/~captainepoch/husky)
[![Download F-Droid](https://img.shields.io/badge/Download-F--Droid-blue)][husky_fdroid]
[![Download Google Play](https://img.shields.io/badge/Download-Play-blue)][husky_playstore]

Husky is a fork of [Tusky][tusky_github] aimed to support [Pleroma's
Mastodon API extensions][mastodon_api_extensions] and whatever could add value
to the application.

# Main changes so far

- Emoji reactions support.
- Removed attachment limits for Pleroma.
- Support for attaching anything on Pleroma.
- Support for changing OAuth application name.
- Markdown support with WYSIWYG editor.
- Support for extended accounts fields, so you can see who is admin or
  moderator.
  on your instance.
- Subscribing support to annoy you with incoming notification from every post.
  (upstreamed to Tusky).
- Support for seen notifications to less annoy you.
- "Reply to" feature that allows to jump to replied status, useful for
  hellthreading.
- Bigger emojis!.
- "Preview" feature on Pleroma.

# Support

Please take a look at [Husky doc.][husky_man] to contribute to the project:

- See [Contributing][husky_man_contributing] to send emails and see the
  discussion about issues and new stuff in the project.
- See [Bug reports][husky_man_bugreport] to report bugs and see TODOs.

Current issue tracker is at [todo.sr.ht/~captainepoch/husky][husky_todo].

Original tracker with issues is at
[git.mentality.rip/FWGS/Husky/issues][husky_original_issues]. Issues to fix will
be taken from there.

# Acknowledgements

The current maintainer is [captainepoch@stereophonic.space][husky_maintainer].
The previous maintainer is
[a1ba@expired.mentality.rip][husky_previous_maintainer].

The original app was developed by
[Vavassor@mastodon.social][tusky_original_dev].
Tusky's maintainer is [ConnyDuck@chaos.social][tusky_maintainer].

# License

[GNU GPL v3][copying].

[copying]: https://git.sr.ht/~captainepoch/husky/tree/master/item/COPYING
[husky_fdroid]: https://f-droid.org/repository/browse/?fdid=su.xash.husky
[husky_maintainer]: https://stereophonic.space/captainepoch
[husky_man]: https://man.sr.ht/~captainepoch/husky-man/
[husky_man_bugreport]: https://man.sr.ht/~captainepoch/husky-man/bugreport.md
[husky_man_contributing]: https://man.sr.ht/~captainepoch/husky-man/contributing.md
[husky_original_issues]: https://git.mentality.rip/FWGS/Husky/issues
[husky_playstore]: https://play.google.com/store/apps/details?id=su.xash.husky
[husky_previous_maintainer]: https://expired.mentality.rip/users/a1ba
[husky_todo]: https://todo.sr.ht/~captainepoch/husky
[mastodon_api_extensions]: https://docs-develop.pleroma.social/backend/development/API/differences_in_mastoapi_responses/
[tusky_github]: https://github.com/tuskyapp/Tusky
[tusky_maintainer]: https://chaos.social/@ConnyDuck
[tusky_original_dev]: https://mastodon.social/@Vavassor
