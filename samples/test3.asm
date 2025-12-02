# Test Program 3: Integer operations for loop simulation
# Note: Branch not fully implemented yet, but integer ops work

DADDI R1, R0, 10    # R1 = 10 (loop counter)
DADDI R3, R0, 0     # R3 = 0 (accumulator)
L.D F0, 0(R2)       # Load initial value
ADD.D F2, F0, F0    # F2 = F0 + F0 (double it)
MUL.D F4, F2, F0    # F4 = F2 * F0
S.D F4, 16(R2)      # Store result
