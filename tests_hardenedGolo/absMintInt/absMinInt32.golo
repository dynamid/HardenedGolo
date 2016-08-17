# Run : ./runWithDistri.sh golo --files absMintInt/absMinInt3.golo
# Check : ./runWithDistri.sh verify --files absMintInt/absMinInt32.golo
#    Println has to be removed to make the check succeed
#    Result : Production of result.mlw
#    Check by : why3 ide result.mlw



module test.MinInt

# function myAbs = |x| spec/ ensures{
function myAbs = |x| spec/ requires{
                               to_int x >= -2147483647
                           }
                           ensures{
                               (to_int result >= 0) /\
                               (to_int result = to_int x \/ to_int result = -(to_int x))}
                     /spec {
	if (x < 0){
		return (0 - x)
	} else {
		return x
	}
}

function princ = {
    # Exemple avec MinInt
    #    => Par construction, la constante "-2147483648" génère une 
    #       erreur dans Why, car on génère -(2147483648), qui n'est 
    #       pas représentable
    let x = -2147483647
    let y = x - 1 
	let a = myAbs(y)  
    
    # Des exemples qui sont corrects
    #let a = myAbs(-2147483647)
	#let a = myAbs(3)
	return (a)
}

function main = |args| {
    let a = princ()
#    println(a)
}
