# Test Program 2: More complex with additional operations
# Demonstrates integer and FP operations together

L.D F6, 0(R2)       # Load F6 from 0(R2)
ADD.D F7, F1, F3    # F7 = F1 + F3
L.D F2, 20(R2)      # Load F2 from 20(R2)
MUL.D F0, F2, F4    # F0 = F2 * F4
SUB.D F8, F2, F6    # F8 = F2 - F6
DIV.D F10, F0, F6   # F10 = F0 / F6
S.D F10, 0(R2)      # Store F10
