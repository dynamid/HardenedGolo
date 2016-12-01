module spec.Div

function notZero = |value| {
  return (value != 0)
}

function printDivTouple = |devidend, iterable| spec/
                                                  requires {
                                                    forall iterable.(notZero(value))
                                                  }
                                              /spec {
  foreach value in iterable {
    println(value)
  }
}

function test = {
	var myDiv = divTouple(20, [2, 0, 4])
	return (myDiv)
}

function main = |args| {
	test()
}
