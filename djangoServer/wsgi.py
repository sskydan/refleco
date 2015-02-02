"""
WSGI config for reflecoDjangoServer project.

It exposes the WSGI callable as a module-level variable named ``application``.

For more information on this file, see
https://docs.djangoproject.com/en/1.6/howto/deployment/wsgi/
"""

import os, sys
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "settings")
# do not change, unless you are me
sys.path.append("/var/www/reflecho.com/djangoServer")

from django.core.wsgi import get_wsgi_application
application = get_wsgi_application()
