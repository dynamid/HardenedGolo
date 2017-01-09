module spec.Div

function div = |a, b| spec/
                        requires {
                          b < 0 \/ (c \/ 5)
                        }
                        ensures {
                          (b = 1 \/ result = a) /\ (b > 1 \/ result < a) /\ (b < 1 \/ result > a) /\ false
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
