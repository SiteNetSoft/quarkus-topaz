package todoApp.GET.todos

import future.keywords.if

default allowed = false

allowed if {
	input.user.id != ""
}
