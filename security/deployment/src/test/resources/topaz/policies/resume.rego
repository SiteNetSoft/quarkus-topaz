package todoApp.GET.resume.where

import future.keywords.if

default allowed = false

allowed if {
	input.user.id != ""
}
