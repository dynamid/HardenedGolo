module ifelsif.impots
# Run : ./runWithDistri.sh golo --files ifelsif/impots.golo
# Check : ./runWithDistri.sh verify --files ifelsif/impots.golo
#    Println has to be removed to make the check succeed
#    Result : Production of result.mlw
#    Check by : why3 ide result.mlw


----
  n : # of persons in the family
  r : money
----
function calculImpot = |n, r| spec/
            requires{
                (n > 0) /\ (r >= 0) /\ (n<15)
            }
            ensures{
                (result >= 0) /\
                (r=0 -> (result <= r )) /\
                (r>0 -> (result < r ))
            }
        /spec {
# function calculImpot = |n, r| spec/
#             requires{
#                 ((to_int n) > 0) /\ ((to_int r) >= 0) /\ (to_int n<15)
#             }
#             ensures{
#                 ((to_int result) >= 0) /\
#                 ((to_int r)=0 -> ((to_int result) <= (to_int r ))) /\
#                 ((to_int r)>0 -> ((to_int result) < (to_int r )))
#             }
#         /spec {
    let ratio = r/n
    var impot = 0

    if(r/16 - 25*n < 0) {
        impot = 0

    } else if(ratio <=20000) {
        impot = r/16 - 25*n

    } else if(ratio <=30000) {
        impot = r/8 - 50*n

    } else if(ratio <=40000) {
        impot = r/4 - 100*n

    } else {
        impot = r/2 - 200*n
    }

	return (impot)
}

function main = |args| {
    #let a = calculImpot(1, 40000)
    var x = 0
    calculImpot(1, 40000)
    calculImpot(1, 40000)
    x=x+1
    # println(a)
}
