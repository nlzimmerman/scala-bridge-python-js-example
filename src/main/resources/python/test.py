import six
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
			raise Exception("We need a startTime to return a timedelta if the duration contains Months or Years!")
		else:
			return x.totimedelta(start=isodate.parse_datetime(startTime))
