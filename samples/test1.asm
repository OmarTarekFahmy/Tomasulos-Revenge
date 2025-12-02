if (Test-Path "c:\Users\Abdelrahman Wael\Documents\Tomasulos-Revenge\out") { Remove-Item -Recurse -Force "c:\Users\Abdelrahman Wael\Documents\Tomasulos-Revenge\out" }# Test Program 1: Basic FP operations with RAW hazards
# This program demonstrates:
# - Load operations
# - FP arithmetic with dependencies
# - Store operation

L.D F6, 0(R2)       # Load F6 from memory
L.D F2, 8(R2)       # Load F2 from memory
MUL.D F0, F2, F4    # F0 = F2 * F4 (RAW on F2)
SUB.D F8, F2, F6    # F8 = F2 - F6 (RAW on F2, F6)
DIV.D F10, F0, F6   # F10 = F0 / F6 (RAW on F0, F6)
ADD.D F6, F8, F2    # F6 = F8 + F2 (WAW on F6, RAW on F8, F2)
S.D F6, 8(R2)       # Store F6 to memory
