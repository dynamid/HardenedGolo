module div.Main

function div = |a, b| {	
   	if(b==0){
		return 1000
	} else {
		return (a/b)
	}
}


function test = {
	var myDiv = div(1,2)
	myDiv = div(40, 20)
	return(myDiv)
}
