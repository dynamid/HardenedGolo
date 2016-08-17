module max.Main

function max = |a, b| {
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


