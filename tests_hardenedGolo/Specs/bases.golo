# Run : ./runWithDistri.sh golo --files Specs/bases.golo
# Check : ./runWithDistri.sh verify --files Specs/bases.golo
#    Println has to be removed to make the check succeed
#    Result : Production of result.mlw
#    Check by : why3 ide result.mlw



module specs.bases

function f = |x,y| spec/  requires{
                               4 >= 3
                           } /spec {
                    #        ensures{
                    #            (result >= 0) /\
                    #            (result = x \/ result = -x)}
                    #  /spec {
	if (x < y){
		return (0 - x)
	} else {
		return x
	}
}

function main = |args| {
    f(42,35)
}
