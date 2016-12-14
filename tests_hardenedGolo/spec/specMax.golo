module max.Main

function max = |a, b| spec/
                        ensures{
                          result >= a
                          /\ result >= b
                          /\ exists z:int.(z >= a /\ z >= b -> z >= result)
									      }
                      /spec {
    if(not(a)) {
      println("OK")
    }
    if(a >= b) {
		return (a)
	} else {
		return (b)
	}
}

function someFunction = |a, b| spec/
                                  ensures{
                                    a \/ b
                                  }
                               /spec {
    if(not(a)) {
      println("OK")
    }
    if(a >= b) {
		return (a)
	} else {
		return (b)
	}
}

function main = |args| {
	var myMax = max(1,2)
	let cons = 40
	myMax = max(cons, 20)
	return(myMax)


	let closure = |x| spec/
	                    ensures {
	                      x >= 0
	                    }
	                  /spec {
    println(x)
    return not(x)
  }

}


