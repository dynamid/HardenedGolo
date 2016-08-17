module max.Comp

function max = |a, b| {

    if(a >= b){
		return (a)
	} else {
		return (b)
	}
}


function switchInt = |myInt| {
	let tmp = myInt: b()
	myInt: b(myInt: a())
	myInt: a(tmp)
}

struct MyInt = {a, b}

function main = |args| {
	let myInt = MyInt(1,2)
	let myMax = max(myInt: a(), myInt: b())
	switchInt(myInt)
	println(myInt: a())
	println(myInt: b())
}
