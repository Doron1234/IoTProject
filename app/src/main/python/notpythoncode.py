def m(a1, a2, a3):
    # Read acceleration values from sensor
    # Calculate the difference with the previous values
    diff1 = a2 - a1
    diff2 = a2 - a3
    threshold = 12
    #Check if the difference and if a2 is above the threshold a
    if diff1 > 0 and diff2 > 0 and a2 > threshold:
        return 1
    return 0