package todoApp.GET.visibleonly

import future.keywords.if

default allowed = false

default visible = false

visible if input.user.id == "rick@the-citadel.com"
