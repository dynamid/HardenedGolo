module fct.Main

function sameValue = |a| {
    var b = a
	if(a > 100) {
		b = one() #requires forward declaration
		return 100
	}
	return b
}

function one = {
	let a = 1 #sameValue(1)
	return a
}

function main = {
	var myVar = sameValue(5)
	let myCons = one()
	var myVar2 = sameValue(101)
	myVar = sameValue(7)
	#test() #Function that doesn't exist
	#Closure + call on ref: let a = |u,o| {return u+o} // let v = a(2,3)
	#println (myVar)
	#println (myCons)
}
