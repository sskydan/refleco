"""
Django settings for reflecoDjangoServer project.

For more information on this file, see
https://docs.djangoproject.com/en/1.6/topics/settings/

For the full list of settings and their values, see
https://docs.djangoproject.com/en/1.6/ref/settings/
"""

# Build paths inside the project like this: os.path.join(BASE_DIR, ...)
import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
FILES_DIR = PROJECT_DIR
LOG_DIR = FILES_DIR + '/static/log/'
GRAMMAR_DIR = FILES_DIR + '/static/nlp/'
CORE_HOST = "http://localhost:7801/engine/"
LIBRARY_HOST = "http://localhost:7800/finbase/"

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = 'eb1ys^n3$^6x*b@a7o%ltq4ysv69+hqi*xu5oqst73@vu&dl=i'

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = True

TEMPLATE_DEBUG = False

ALLOWED_HOSTS = ['*']

# Application definition

INSTALLED_APPS = (
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'reflecoSearch',
)

MIDDLEWARE_CLASSES = (
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
)

DATABASES = {
    'default': {
        'NAME': PROJECT_DIR + "/article.db",
        'ENGINE': 'django.db.backends.sqlite3',
        'HOST': '',
        'USER': '',
        'PASSWORD': '',
    }
}

DEFAULT_INDEX_TABLESPACE = 'article'

ROOT_URLCONF = 'urls'

WSGI_APPLICATION = 'wsgi.application'

EMAIL_USE_TLS = True
EMAIL_HOST = 'smtp.gmail.com'
EMAIL_PORT = 587
EMAIL_HOST_USER = 'info@reflecho.com'
EMAIL_HOST_PASSWORD = 'remindertodonate'

# Internationalization
LANGUAGE_CODE = 'en-us'
TIME_ZONE = 'UTC'
USE_I18N = True
USE_L10N = True
USE_TZ = True

#template files (.html)
TEMPLATE_URL = '/templates/'
TEMPLATE_DIRS = (
    PROJECT_DIR + '/templates/',
)

# Static files (CSS, JavaScript, Images)
STATIC_ROOT = ''
STATIC_URL = '/static/'
STATICFILES_DIRS = (
    PROJECT_DIR + '/static/',
)


LOGGING = {
    'version': 1,
    'disable_existing_loggers': True,
    'filters': {
        'require_debug_false': {
            '()': 'django.utils.log.RequireDebugFalse',
        },
        'require_debug_true': {
            '()': 'django.utils.log.RequireDebugTrue',
        },
    },
    'formatters': {
        'simple': {
            'format': '[%(asctime)s] %(levelname)s %(message)s',
	    'datefmt': '%Y-%m-%d %H:%M:%S'
        },
        'verbose': {
            'format': '[%(asctime)s] %(levelname)s [%(name)s.%(funcName)s:%(lineno)d] %(message)s',
	    'datefmt': '%Y-%m-%d %H:%M:%S'
        },
    },
    'handlers': {
        'console': {
            'level': 'DEBUG',
            'filters': ['require_debug_true'],
            'class': 'logging.StreamHandler',
            'formatter': 'simple'
        },
        'development_logfile': {
            'level': 'DEBUG',
            'filters': ['require_debug_true'],
            'class': 'logging.FileHandler',
            'filename':  LOG_DIR + 'dev.log',
            'formatter': 'verbose'
        },
        'query_logfile': {
            'level': 'DEBUG',
            'filters': [],
            'class': 'logging.FileHandler',
            'filename':  LOG_DIR + 'query.log',
            'formatter': 'verbose'
        }
    },
    'loggers': {
        'console':{
            'handlers': ['console'],
            'level': 'DEBUG',
        },
        'query': {
            'handlers': ['console', 'query_logfile'],
            'level': 'INFO'
         },
        'development': {
            'handlers': ['console','development_logfile'],
            'level': 'DEBUG',
        },
    }
}