package todoApp.GET.secured.todos

import future.keywords.if

default allowed = false

allowed if {
	input.user.id != ""
}
