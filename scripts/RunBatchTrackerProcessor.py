import os, sys
import com.optofluidics.app.OptofluidicsBatchProcessor_ as OptofluidicsBatchProcessor_

# Expecting one argument: the file path
if len(sys.argv) < 2:
  print "Usage: ./<path-toImageJ-executable> --headless RunBatchTrackerProcessor.py <folder-path>"
  sys.exit(1)
 
filepath = sys.argv[1]
 
# Check if the file exists
if not os.path.exists(filepath):
  print "File does not exist at path:", filepath
  sys.exit(1)

# Compase argument string
arg = 'folder=[' + filepath +']'
if len(sys.argv) > 2:
	arg = arg + ' parameters=' + sys.argv[2]

print(arg)

# execute
print("Starting batch processing.")
OptofluidicsBatchProcessor_().run( arg );
print("Done.")