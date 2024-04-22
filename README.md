# Husky

[![Download F-Droid](https://img.shields.io/badge/Download-F--Droid-blue)][husky_fdroid]
[![Download Google Play](https://img.shields.io/badge/Download-Play-blue)][husky_playstore]
[![Translation
status](https://hosted.weblate.org/widget/husky/translations/svg-badge.svg)](https://hosted.weblate.org/engage/husky/)

Husky is a fork of [Tusky][tusky_github] aimed to support [Pleroma's Mastodon
API extensions][mastodon_api_extensions] and whatever could add value to the
application.

# Main changes so far

- Emoji reactions support.
- Removed attachment limits for Pleroma.
- Support for attaching anything on Pleroma.
- Support for changing OAuth application name.
- Markdown support with WYSIWYG editor.
- Support for extended accounts fields, so you can see who is admin or
  moderator. on your instance.
- Subscribing support to annoy you with incoming notification from every post.
  (upstreamed to Tusky).
- Support for seen notifications to less annoy you.
- "Reply to" feature that allows to jump to replied status, useful for
  hellthreading.
- Bigger emojis!.
- "Preview" feature on Pleroma.

# Support

You can support the project by contributing to it. Look at the
[Contributing][husky_man_contributing] documentation on how to open issues or
fix issues.

Previous issue tracker is at
[codeberg.org/husky/husky/issues][husky_codeberg_issues].

Original tracker with issues is at
[git.mentality.rip/FWGS/Husky/issues][husky_original_issues].

# Acknowledgements

The current maintainer is [captainepoch@stereophonic.space][husky_maintainer].
The previous maintainer is
[a1ba@suya.place][husky_previous_maintainer].

The original app was developed by
[Vavassor@mastodon.social][tusky_original_dev]. Tusky's maintainer is
[ConnyDuck@chaos.social][tusky_maintainer].

# License

[GNU GPL v3][copying].

[copying]: ./COPYING
[husky_codeberg_issues]: https://codeberg.org/husky/husky/issues
[husky_fdroid]: https://f-droid.org/repository/browse/?fdid=su.xash.husky
[husky_maintainer]: https://stereophonic.space/captainepoch
[husky_man]: https://github.com/captainepoch/husky/wiki
[husky_man_contributing]: https://github.com/captainepoch/husky/wiki/Contributing-to-Husky
[husky_original_issues]: https://git.mentality.rip/FWGS/Husky/issues
[husky_playstore]: https://play.google.com/store/apps/details?id=su.xash.husky
[husky_previous_maintainer]: https://suya.place/users/a1ba
[husky_todo]: https://github.com/captainepoch/husky/issues
[mastodon_api_extensions]: https://docs-develop.pleroma.social/backend/development/API/differences_in_mastoapi_responses/
[tusky_github]: https://github.com/tuskyapp/Tusky
[tusky_maintainer]: https://chaos.social/@ConnyDuck
[tusky_original_dev]: https://mastodon.social/@Vavassor
