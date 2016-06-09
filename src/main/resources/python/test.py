from __future__ import absolute_import, division, print_function, unicode_literals
# http://python-future.org/compatible_idioms.html
# NB: from __future__ import unicode_literals
# seems not to work correctly in jython, so we have to use six to hack around it.

from future.utils import raise_with_traceback
# for some reason, builtins doesn't seem to be importable in jython, so we have
# to use six to hack around that too.
from six import integer_types, text_type, binary_type

# Just in case you need to step through an iterator
from six import next

# I'm not impoting the following since it breaks the syntax semi-unnecessarily
# (Py3 syntax works on Py2 but is memory-inefficient)
# but be aware of them if you need to deal with large dicts
# from six import iterkeys, itervalues, iteritems,

# Because from __future__ import unicode_literals seems to work incorrectly,
# we need to observe the following rules (1–4 are good ideas anyway, 5
# is specific to the fact that builtins doesn't work right in jython).
# 1. Strings must always be declared as unicode (u'foo') if they are going be returned.
# 2. byte-arrays must always be declared with a b''.
# 3. use decode and encode to go from bytes to strings and strings to bytes
# 4. Classes must always inherit from _something_, usually object.
# 5. Type-checking stinks, but there's nothing we can do about it:
#    string: type(x) is six.text_type
#    bytes:  type(x) is six.binary_type
#    int:    isinstance(x, six.integer_types)

# The following imports are optional — they replace memory-inefficient py2 functions
# with syntax-identical but memory-efficient py3 functions
from six.moves import range, zip

# This is a solution to specific to a problem I had. I hope you don't have the
# same one, but if you do, this might help.
#
# from six import StringIO, BytesIO
# from six import PY2
# if PY2:
# 	from zipfile import error as BadZipFile
# else:
# 	from zipfile import BadZipFile

import isodate
import datetime
import pytz
import sys
import test_module.test

class Test(object):
	def __init__(self):
		pass
	def moduleTest(self):
		T = test_module.test.TestClass()
		return T.test()
	def plusTwo(self,x):
		return x+2
	def rightNow(self):
		return datetime.datetime.utcnow()
	def parseDurationString(self, durationString, startTime=None):
		x = isodate.parse_duration(durationString)
		if type(x) is datetime.timedelta:
			return x
		elif startTime is None:
			raise_with_traceback(
				Exception("We need a startTime to return a timedelta if the duration contains Months or Years!")
			)
		else: # the type should be isodate.Duration, though I'm not checking!
			return x.totimedelta(start=isodate.parse_datetime(startTime))
