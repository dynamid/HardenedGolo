module max.Main

function max = |a, b| spec/ ensures{ (result >= a) /\
									(result >= b) /\
									(forall z:int. z>= a /\
									z >= b -> z >= result) } /spec {
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
}


