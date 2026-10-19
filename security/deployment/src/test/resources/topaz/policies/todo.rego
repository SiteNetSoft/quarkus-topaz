package todoApp.GET.todos

import future.keywords.if

default allowed = false

allowed if {
	input.user.id != ""
}

default visible = false

visible if input.user.id == "rick@the-citadel.com"
