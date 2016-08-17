module max.Func

function main = |args| {
    
    println(max(2, 10))

}

function max = |a, b| {

    if(a >= b){
		return (a)
	} else {
		return (b)
	}
}
