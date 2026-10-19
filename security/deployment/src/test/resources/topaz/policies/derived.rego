package todoApp.GET.todos.derived

import future.keywords.if

default allowed = false

allowed if {
	input.user.id != ""
}
