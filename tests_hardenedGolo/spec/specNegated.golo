module spec.Div

function minus = |value| {
  return 0 - value
}

function some = |a, b| spec/
                        requires {
                          b - minus(5) > (!minus(b))
                        }
                     /spec {
  return (a / b)
}

function div = |a, b| spec/
                        requires {
                          b - (!5) > 5
                        }
                     /spec {
  return (a / b)
}

function test = {
	var myDiv = div(1, 2)
	myDiv = div(40, 1)
	return(myDiv)
}

function main = |args| {
	test()
}
