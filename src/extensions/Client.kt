package extensions

import models.Client


fun Client.toPair(): Pair<String, Client> = userName to this
